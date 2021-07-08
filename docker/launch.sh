docker run -d -p 8000:8000 amazon/dynamodb-local

aws dynamodb create-table --table-name kf-key-management-secret --attribute-definitions AttributeName=userId,AttributeType=S AttributeName=service,AttributeType=S --key-schema AttributeName=userId,KeyType=HASH AttributeName=service,KeyType=RANGE --billing-mode PAY_PER_REQUEST --endpoint-url http://localhost:8000