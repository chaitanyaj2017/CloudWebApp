#!/bin/bash
# Script to delete the networking resources


region="us-east-1"

if [ $# -eq 0 ]; then
 echo " PLEASE PASS <VPC_NAME> as parameter while running this script "
 exit 1
fi

echo "Prepare for deleting,please wait........"

vpc="$1-csye6225-vpc"
vpcname=$(aws ec2 describe-vpcs \
 --query "Vpcs[?Tags[?Key=='Name']|[?Value=='$vpc']].Tags[0].Value" \
 --output text)
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while finding vpc name"
        exit $ret
fi
echo "vpc Name:"
echo "$vpcname"

vpc_id=$(aws ec2 describe-vpcs \
 --query 'Vpcs[*].{VpcId:VpcId}' \
 --filters "Name=tag-value,Values="$vpcname"" \
 --output text \
  --region $region)
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while finding vpc id"
        exit $ret
fi
echo "vpc Id:"
echo "$vpc_id"

route_tbl_id=$(aws ec2 describe-route-tables \
 --filters "Name=vpc-id,Values=$vpc_id" "Name=association.main, Values=false" \
 --query 'RouteTables[*].{RouteTableId:RouteTableId}' \
 --output text)
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while finding route table id"
        exit $ret
fi
echo "Route Table Id:"
echo "$route_tbl_id"

IGW_Id=$(aws ec2 describe-internet-gateways \
  --query 'InternetGateways[*].{InternetGatewayId:InternetGatewayId}' \
  --filters "Name=attachment.vpc-id,Values=$vpc_id" \
  --output text)
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while finding internet gateway"
        exit $ret
fi
echo "Internet Gateway Id:"
echo "$IGW_Id"

echo "Start to delete!!"
while
sub=$(aws ec2 describe-subnets \
 --filters Name=vpc-id,Values=$vpc_id \
 --query 'Subnets[*].SubnetId' \
 --output text)
et=$?
	if [ $ret -ne 0 ];
	then
        	echo "Error while finding subnet"
        	exit $ret
	fi
[[ ! -z $sub ]]
do
        var1=$(echo $sub | cut -f1 -d" ")
       # echo $var1 is deleted
        aws ec2 delete-subnet --subnet-id $var1
	ret=$?
	if [ $ret -ne 0 ];
	then
        	echo "Error while deleting subnet"
        	exit $ret
	fi
done
echo "Subnets deleted---------------------"

aws ec2 delete-route-table --route-table-id $route_tbl_id
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while deleting route table"
        exit $ret
fi
echo "Route-Table deleted-----------------------"

aws ec2 detach-internet-gateway \
 --internet-gateway-id $IGW_Id \
 --vpc-id $vpc_id
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while detaching internet gateway"
        exit $ret
fi
echo "IGW detached------------------------"

aws ec2 delete-internet-gateway \
 --internet-gateway-id $IGW_Id
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while deleting internet gateway"
        exit $ret
fi
echo "IGW delete------------------------"

aws ec2 delete-vpc --vpc-id $vpc_id
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error while deleting vpc"
        exit $ret
fi
echo "VPC delete------------------------"
echo "Complete!!"