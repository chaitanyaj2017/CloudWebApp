#!bin/bash
if [ $# -eq 0 ];
then
        echo "Please provide a stack name"
        exit 1
fi
stack_name=$1
aws cloudformation delete-stack --stack-name $stack_name
ret=$?
if [ $ret -eq 0 ];
then
        echo "stack is being deleted...."
	aws cloudformation wait stack-delete-complete --stack-name $stack_name	
	echo "stack deleted successfully!!"
else
       	echo "stack could not be deleted"
fi
