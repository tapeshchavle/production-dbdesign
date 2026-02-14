#!/bin/bash
# LocalStack initialization — creates SNS topics, SQS queues, and DLQs
# This runs automatically when LocalStack starts

echo "Creating SNS topics..."
awslocal sns create-topic --name order-events --region ap-south-1
awslocal sns create-topic --name user-events --region ap-south-1
awslocal sns create-topic --name catalog-events --region ap-south-1

echo "Creating SQS Dead Letter Queues..."
awslocal sqs create-queue --queue-name notification-dlq --region ap-south-1

echo "Creating SQS Queues with DLQ..."
awslocal sqs create-queue --queue-name notification-queue \
  --attributes '{
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:ap-south-1:000000000000:notification-dlq\",\"maxReceiveCount\":\"3\"}"
  }' --region ap-south-1

echo "Subscribing SQS queues to SNS topics..."
NOTIFICATION_QUEUE_ARN="arn:aws:sqs:ap-south-1:000000000000:notification-queue"

awslocal sns subscribe \
  --topic-arn arn:aws:sns:ap-south-1:000000000000:order-events \
  --protocol sqs \
  --notification-endpoint $NOTIFICATION_QUEUE_ARN \
  --region ap-south-1

awslocal sns subscribe \
  --topic-arn arn:aws:sns:ap-south-1:000000000000:user-events \
  --protocol sqs \
  --notification-endpoint $NOTIFICATION_QUEUE_ARN \
  --region ap-south-1

awslocal sns subscribe \
  --topic-arn arn:aws:sns:ap-south-1:000000000000:catalog-events \
  --protocol sqs \
  --notification-endpoint $NOTIFICATION_QUEUE_ARN \
  --region ap-south-1

echo "Verifying SES email identity..."
awslocal ses verify-email-identity --email-address noreply@ecommerce.com --region ap-south-1

echo "✅ LocalStack initialization complete!"
echo "Topics: order-events, user-events, catalog-events"
echo "Queues: notification-queue → DLQ: notification-dlq (maxReceiveCount: 3)"
echo "SES: noreply@ecommerce.com verified"
