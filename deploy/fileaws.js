import {
  PutObjectCommand,
  ListObjectsCommand,
  S3Client,
} from "@aws-sdk/client-s3";

import { readFileSync } from "fs";
import { curly } from "node-libcurl";

const TARGET_BUCKET = process.env.TARGET_BUCKET;
if (TARGET_BUCKET === undefined) {
  console.error("must define environmental variable TARGET_BUCKET");
  process.exit(3);
} else {
  console.log(`Uploads will target S3 bucket ${TARGET_BUCKET}.`);
}

const client = new S3Client({
  apiVersion: "2006-03-01",
  region: "eu-central-1",
});

/**
 * Perform S3 put object command. Catch error and log.
 *
 * @param  fileName - name of file to put
 * @param ContentType - file MIME type
 */
export const tryPut = async (fileName, ContentType = undefined) => {
  const command = new PutObjectCommand({
    Bucket: TARGET_BUCKET,
    Key: fileName,
    Body: readFileSync(fileName),
    ACL: "public-read",
    ContentType: ContentType,
  });

  try {
    const response = await client.send(command);
  } catch (err) {
    console.error(err);
  }
};

/**
 * Download file, then upload to s3.
 *
 * @param url - url of remote file
 * @param filename  - name of file to bot hdow
 * @param foldername - s3 file upload to this folder
 * @param contentType  - s3 file MIME type
 */
export const tryPutRemote = async (
  url,
  filename,
  foldername,
  contentType = undefined
) => {
  const { statusCode, data, headers } = await curly.get(url, {
    FOLLOWLOCATION: true,
    HTTPHEADER: [`Authorization: ${process.env.GITHUB_TOKEN}`],
  });

  if (statusCode != 200) {
    console.error(
      `Unexpected Response from for URL: ${url} status: ${statusCode}`
    );
    return;
  }

  const command = new PutObjectCommand({
    Bucket: TARGET_BUCKET,
    Key: foldername + "/" + filename,
    Body: data,
    ACL: "public-read",
    ContentType: contentType,
  });

  try {
    const response = await client.send(command);
    console.log(response);
  } catch (err) {
    console.error(err);
    process.exit(-1);
  }
};

/**
 * List object from S3 bucket
 *
 * @returns ListObjectCommandOutput
 */
export const tryListObjects = async () => {
  const command = new ListObjectsCommand({ Bucket: TARGET_BUCKET });

  try {
    const response = await client.send(command);
    console.log(response);
    return response;
  } catch (err) {
    console.error(err);
    process.exit(-1);
  }
};
