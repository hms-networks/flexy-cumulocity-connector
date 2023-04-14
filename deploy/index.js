import { Octokit } from "octokit";
import { writeFileSync, readFileSync } from "fs";
import { tryPut, tryPutRemote } from "./fileaws.js";
import sgMail from "@sendgrid/mail";

const MIME_JSON = "application/json";
const MANIFEST_FILE_NAME = "manifest.json";
const LATEST_FILE_NAME = "latest.json";

const DEFAULT_JSON_INDENT = 2;
const MIME_JAR = "application/java-archive";
const MIME_TXT = "text/plain";

const S3_BASE_URL = process.env.S3_BASE_URL;
if (S3_BASE_URL === undefined) {
  console.error("Error reading environment variable S3_BASE_URL.");
  process.exit(4);
}

const TARGET_REPO = "/repos/" + process.env.GITHUB_REPOSITORY;
if (TARGET_REPO === "/repos/") {
  console.error("Error reading environment variable GITHUB_REPOSITORY.");
  process.exit(5);
}

const REPO_OWNER = process.env.GITHUB_REPOSITORY_OWNER;
if (REPO_OWNER === undefined) {
  console.error("Error reading environment variable GITHUB_REPOSITORY_OWNER.");
  process.exit(6);
}

const GITHUB_HEADERS = {
  "X-GitHub-Api-Version": "2022-11-28",
};

/* Some tags should not be considered, if the tag_name includes any of the following strings, it should be filtered */
const FILTER_TAG_STRINGS = ["pre", "beta", "alpha"];

const octokit = new Octokit({
  auth: process.env.GITHUB_TOKEN,
});

/**
 * Make Github API request for information on releases
 *
 * @param  owner - owner of the repo
 * @param  repoName - name of the repo
 * @returns Promise<OctokitResponse<any>>
 */
const makeGithubApi = async () => {
  const resp = await octokit.request("GET " + TARGET_REPO + "/releases", {
    owner: "OWNER",
    repo: "REPO",
    headers: GITHUB_HEADERS,
  });
  return resp;
};

/**
 * Make Github API request for information on latest releases
 *
 * @param  owner - owner of the repo
 * @param  repoName - name of the repo
 * @returns Promise<OctokitResponse<any>>
 */
const makeGithubApiLatestRequest = async () => {
  const resp = await octokit.request(
    "GET " + TARGET_REPO + "/releases/latest",
    {
      owner: "OWNER",
      repo: "REPO",
      headers: GITHUB_HEADERS,
    }
  );
  return resp;
};

/**
 * Verify response object, return data if possible.
 *
 * @param  resp - OctoKitResponse
 * @returns data, or undefined
 */
const getDataFromResp = (resp) => {
  if (resp && resp.status == 200) {
    // check that data is not undefined and every item in array has a tag_name
    if (
      !resp.data ||
      resp.data.length == undefined ||
      !resp.data.every((d) => d.tag_name)
    ) {
      console.error(
        `Unexpected response from Github API ${JSON.stringify(
          resp,
          null,
          DEFAULT_JSON_INDENT
        )} .`
      );
      process.exit(1);
    }
    return resp.data;
  }
};

/**
 * Verify response object, return releases without "pre", "beta", or "alpha" included in tag_name
 *
 * @param  data - OctoKitResponse.data
 * @returns data, or undefined
 */
const filterNonFinalReleases = (data) => {
  // Filter all releases that are beta, alpha, or pre
  const noPreOrBeta = data.filter((d) => {
    let pass = true;
    for (let subStr of FILTER_TAG_STRINGS) {
      if (d.tag_name.includes(subStr)) {
        pass = false;
      }
    }
    return pass;
  });
  return noPreOrBeta;
};

/**
 * Sort release by date publish data
 *
 * @param  data - OctoKitResponse.data
 * @returns data, or undefined
 */
const sortReleasesByDatePublishDate = (data) => {
  data.sort((a, b) => a.published_at < b.published_at);
  return data;
};

/** From Github API asset response, parse assets to be loaded to s3.
 *
 * @param  assets - object response from Github REST API
 * @param  regex - regular expression for filtering assets
 * @returns
 */
const getAssetLink = (assets, regex) => {
  const jar = assets.filter((a) => regex.test(a.name));
  if (jar.length > 0) {
    let target = jar[0];
    return { name: target.name, download_url: target.browser_download_url };
  }
};

/**
 * From API response, create a list of release names.
 *
 * @param  data - OctoKitResponse.data
 * @returns data, or undefined
 */
const getReleaseData = (data) => {
  // prefer tag_name to name, the latter might include spaces
  const jarReg = /^[\s\S-]*-\d\.\d\.\d-\w+\.jar\b/g;
  const configReg = /^[\s\S]*ConnectorConfig.json/g;
  const jvmRunReg = /^jvmrun/g;
  const names = data.map((d) => {
    return {
      name: d.tag_name,
      jar: getAssetLink(d.assets, jarReg),
      configuration: getAssetLink(d.assets, configReg),
      jvmRun: getAssetLink(d.assets, jvmRunReg),
    };
  });
  return names;
};

/**
 * Filter all release versions that do not include all asset files
 *
 *
 * @param release - Github release data
 *
 * @returns True/False release includes all assets
 */
const FilterOnlyCompleteReleases = (release) => {
  return release && release.jvmRun && release.jar && release.configuration;
};

/**
 * Get the tag name of the latest release
 *
 * @returns tag name or undefined
 */
const getLatestReleaseTagName = async () => {
  try {
    let latest = await makeGithubApiLatestRequest();
    if (latest && latest.status && latest.status === 200 && latest.data) {
      if (latest.data.tag_name) {
        return latest.data.tag_name;
      }
    }
    console.error(
      `Response for latest request unexpected ${JSON.stringify(
        latest,
        null,
        DEFAULT_JSON_INDENT
      )}. `
    );
  } catch (e) {
    console.log(e.message);
    return;
  }
};

/**
 * Urls for assets currently have a url and path for github, this function will
 * return assets with a url and for S3. This function does not modify the passed parameter.
 *
 * @param asset - object
 * @returns asset object with url and paths updated
 */
const transformDownloadUrlToS3Format = (asset) => {
  const downloadUrlBase = S3_BASE_URL + asset.name + "/";
  // one way to deep copy
  let ret = JSON.parse(JSON.stringify(asset));
  ret.jar.download_url = downloadUrlBase + asset.jar.name;
  ret.configuration.download_url = downloadUrlBase + asset.configuration.name;
  ret.jvmRun.download_url = downloadUrlBase + asset.jvmRun.name;
  return ret;
};

/**
 * Request release data and write manifest file.
 *
 * returns true for success or false for failure
 */
const writeReleaseManifestAndLatest = async () => {
  let response;
  try {
    response = await makeGithubApi();
  } catch (e) {
    console.error("Unable to request Github API data.");
    console.error(e.message);
    process.exit(-1);
  }
  console.log(JSON.stringify(response, null, DEFAULT_JSON_INDENT));

  const releases = filterNonFinalReleases(getDataFromResp(response));
  const res = getReleaseData(sortReleasesByDatePublishDate(releases));
  const filtered = res.filter(FilterOnlyCompleteReleases);
  const transformedToS3 = filtered.map(transformDownloadUrlToS3Format);

  writeFileSync(
    MANIFEST_FILE_NAME,
    JSON.stringify(transformedToS3, null, DEFAULT_JSON_INDENT)
  );

  const latestTagName = await getLatestReleaseTagName();

  let identifyLatestSuccess = false;

  if (latestTagName) {
    const latestIdx = filtered.findIndex(
      (release) => release.name === latestTagName
    );
    if (latestIdx !== -1) {
      writeFileSync(
        LATEST_FILE_NAME,
        JSON.stringify(transformedToS3[latestIdx], null, DEFAULT_JSON_INDENT)
      );
      identifyLatestSuccess = true;
    } else {
      console.error(
        `Could not find latest release version tag in releases ${latestTagName}.`
      );
    }
  }
  //  write the first version in the array !IMPORTANT - releases are sorted in order of release
  if (!identifyLatestSuccess) {
    if (filtered.len > 0) {
      writeFileSync(
        LATEST_FILE_NAME,
        JSON.stringify(transformedToS3[0], null, DEFAULT_JSON_INDENT)
      );
      identifyLatestSuccess = true;
    } else {
      console.error(
        "All features were filtered, or not found in response. Unable to find latest release version."
      );
    }
  }
  let ret = {
    fileWriteSuccess: identifyLatestSuccess,
    releaseData: filtered,
  };
  return ret;
};

/**
 * Upload manifest and JSON file
 *
 */
const uploadManifestAndLatest = () => {
  tryPut(MANIFEST_FILE_NAME, MIME_JSON);
  tryPut(LATEST_FILE_NAME, MIME_JSON);
};

/**
 * Upload Assets
 *
 */
const uploadReleaseAssets = (assets) => {
  for (let asset of assets) {
    const folderName = asset.name;
    // upload Jar
    tryPutRemote(asset.jar.download_url, asset.jar.name, folderName, MIME_JAR);

    // upload configuration
    tryPutRemote(
      asset.configuration.download_url,
      asset.configuration.name,
      folderName,
      MIME_JSON
    );

    // upload jvmrun file
    tryPutRemote(
      asset.jvmRun.download_url,
      asset.jvmRun.name,
      folderName,
      MIME_TXT
    );
  }
};

/**
 * Send email with summary via SendGrid
 *
 */
const sendNotification = () => {
  const file_content = readFileSync("./index.js", "utf-8");
  const content = Buffer.from(file_content).toString("base64");
  const target_emails = process.env.SENDGRID_TARGET_LIST;
  let to_list;

  sgMail.setApiKey(process.env.SENDGRID_API_KEY);

  if (target_emails === undefined) {
    console.error(
      "Notification canceled because SENDGRID_TARTGET_LIST environment var was not set."
    );
    return;
  }
  try {
    to_list = JSON.parse(process.env.SENDGRID_TARGET_LIST);
  } catch (error) {
    console.error(`JSON parsing error on string ${target_emails}`);
    console.error(error);
    return;
  }

  const msg = {
    to: to_list,
    from: "no-reply@hmsamericas.com",
    subject: `Connector update for ${process.env.GITHUB_REPOSITORY} `,
    html: `<div><p>Greetings,</p><p>This is an automated email. The S3 repo ${S3_BASE_URL} for project ${process.env.GITHUB_REPOSITORY} has been updated. The manifest file is attached to this email.</p></div>`,
    attachments: [
      {
        content: content,
        filename: "manifest.json",
        type: MIME_JSON,
        disposition: "attachment",
      },
    ],
  };

  sgMail
    .send(msg)
    .then((response) => {
      console.log(response[0].statusCode);
      console.log(response[0].headers);
    })
    .catch((error) => {
      console.error(error);
      console.error(error.response.body);
    });
};

const { fileWriteSuccess, releaseData } = await writeReleaseManifestAndLatest();
if (fileWriteSuccess) {
  uploadManifestAndLatest();
}
if (releaseData) {
  uploadReleaseAssets(releaseData);
}

sendNotification();
