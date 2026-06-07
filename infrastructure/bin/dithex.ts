#!/usr/bin/env node
import * as cdk from "aws-cdk-lib";
import { DithexStack } from "../lib/dithex-stack";

const app = new cdk.App();

new DithexStack(app, "Dithex", {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: "eu-central-1",
  },
  description: "Static hosting for dithex.oliverblaser.ch",
});
