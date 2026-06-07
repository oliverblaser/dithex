# Dithex AWS Infrastructure

This AWS CDK app creates a single CloudFormation stack in `eu-central-1` for
`dithex.oliverblaser.ch`.

The stack imports the existing public Route 53 hosted zone:

```text
Hosted zone: oliverblaser.ch
Hosted zone ID: Z0985921288XO968I921N
```

It creates:

- Private encrypted S3 bucket in Frankfurt
- Global CloudFront distribution with Origin Access Control
- Route 53 `A` and `AAAA` records for `dithex.oliverblaser.ch`
- HTTPS-only delivery, compression, caching, and security response headers

The public domain, hosted-zone details, AWS account ID, and certificate ARN are intentionally
committed because they are identifiers rather than credentials. No AWS credentials, private
keys, bucket name, or distribution ID is stored in this repository.

## Prerequisites

- Node.js 22 or newer with npm
- AWS CLI configured with a local profile
- The `oliverblaser.ch` registrar delegates the full domain to the Route 53 name servers
- A validated ACM certificate for `dithex.oliverblaser.ch` created manually in `us-east-1`

CloudFront requires its viewer certificate to be in `us-east-1`. The certificate can still
be referenced by a CloudFormation stack deployed in Frankfurt.

## First Deployment

From `infrastructure/`, install dependencies:

```bash
npm install
```

Bootstrap Frankfurt once:

```bash
npx cdk bootstrap aws://ACCOUNT_ID/eu-central-1 --profile YOUR_PROFILE
```

Preview and deploy the single stack:

```bash
npx cdk diff Dithex --profile YOUR_PROFILE
npx cdk deploy Dithex --profile YOUR_PROFILE
```

Record the emitted `SiteBucketName` and `DistributionId` outputs.

## Manual Application Deployment

Build the optimized web application from the repository root:

```bash
./gradlew :webApp:wasmJsBrowserDistribution
```

Upload it using the bucket name emitted by the stack:

```bash
aws s3 sync webApp/build/dist/wasmJs/productionExecutable/ \
  s3://YOUR_SITE_BUCKET/ \
  --delete \
  --profile YOUR_PROFILE
```

Invalidate CloudFront so `index.html` and `webApp.js` update immediately:

```bash
aws cloudfront create-invalidation \
  --distribution-id YOUR_DISTRIBUTION_ID \
  --paths "/*" \
  --profile YOUR_PROFILE
```

## Removing the Stack

```bash
npx cdk destroy Dithex --profile YOUR_PROFILE
```

The S3 bucket uses `RETAIN`, protecting deployed files from accidental deletion.
