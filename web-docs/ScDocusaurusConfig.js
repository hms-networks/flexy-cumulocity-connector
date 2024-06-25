// CONFIGURATION ZONE - YOU CAN EDIT THESE LINES
const ScDocusaurusConfig = {
  repoOwnerName: 'hms-networks',
  repoName: 'flexy-cumulocity-connector',
  title: 'Flexy Cumulocity Connector',
  description: 'Connect your Ewon Flexy to Cumulocity Cloud or Cumulocity Edge with the Flexy Cumulocity Connector.',
  meta: 'Homepage for the HMS Networks Flexy Cumulocity Connector.',
  configFileName : 'CumulocityConnectorConfig.json',
  applicationJarFileName : 'flexy-cumulocity-connector-X.Y.Z-full.jar',
  commonDocRepoTargetBranchOrTag: 'v1.1.0',
  keywords: 'ewon, jtk, java, maven, project, cumulocity, connector, historian, c8y, requirements, troubleshooting, support',
};

// EXPORT ZONE - DON'T TOUCH BELOW THIS LINE
module.exports = {
  ...ScDocusaurusConfig,
  repoUrl: 'https://github.com/' + ScDocusaurusConfig.repoOwnerName + '/' + ScDocusaurusConfig.repoName,
  repoArchiveUrl: 'https://github.com/' + ScDocusaurusConfig.repoOwnerName + '/' + ScDocusaurusConfig.repoName + '/archive/refs/heads/main.zip',
  repoLatestReleaseUrl: 'https://github.com/' + ScDocusaurusConfig.repoOwnerName + '/' + ScDocusaurusConfig.repoName + '/releases/latest',
  repoNewIssueUrl: 'https://github.com/' + ScDocusaurusConfig.repoOwnerName + '/' + ScDocusaurusConfig.repoName + '/issues/new',
  commonDocsRepoUrl: 'https://raw.githubusercontent.com/hms-networks/sc-ewon-flexy-common-docs/' + ScDocusaurusConfig.commonDocRepoTargetBranchOrTag + '/',
};