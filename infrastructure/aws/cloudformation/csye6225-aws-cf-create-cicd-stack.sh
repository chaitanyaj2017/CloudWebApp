#!/bin/bash
PolicyStack=$1

if [ $# -eq 0 ];
then 
        echo "Please enter policy stack name"
        exit 1
fi

aws_domain_name=$(aws route53 list-hosted-zones --query 'HostedZones[0].Name' --output text)
bucketName="code-deploy.${aws_domain_name:0:-1}"
#export bucketName=$(aws s3api list-buckets --query "Buckets[].Name" --output text|grep code-deploy|awk '{print $1}')

UserId="circleci"


#aws cloudformation validate-template --template-body file://testpol.json
aws cloudformation validate-template --template-body file://csye6225-cf-cicd.json >/dev/null 2>&1
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error in template!!"
        exit $ret
fi


aws cloudformation create-stack --stack-name $PolicyStack --template-body file://csye6225-cf-cicd.json --capabilities CAPABILITY_NAMED_IAM --parameters ParameterKey=CircleCIUser,ParameterValue=$UserId ParameterKey=S3CodeBucket,ParameterValue=$bucketName


export STACK_STATUS=$(aws cloudformation describe-stacks --stack-name $PolicyStack --query "Stacks[][ [StackStatus ] ][]" --output text)

while [ $STACK_STATUS != "CREATE_COMPLETE" ]
do
	STACK_STATUS=`aws cloudformation describe-stacks --stack-name $PolicyStack --query "Stacks[][ [StackStatus ] ][]" --output text`
done
echo "Created Stack ${PolicyStack} successfully!"
