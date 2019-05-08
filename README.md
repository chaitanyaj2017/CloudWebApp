# CSYE 6225 - Spring 2019


## Technology Stack
### Operating System
* Linux (Fedora)

### Programming Language
* Java

### Relational Database
* MariaDB

### Backend Framework
* Spring-Boot/Maven

## Build Instructions
### Prerequisites
* Java (v8 or higher)
* Maven CLI
* MariaDB


## Deploy Instructions
* Add the domain name to the environment variable for deploying on circleci
```
    $ AWS_ACCESS_KEY_ID=Y<ACCESS KEY FROM AWS>
    $ AWS_REGION=<us-east-1>
    $ AWS_SECRET_ACCESS_KEY=<SECRET ACCESS KEY FROM AWS>
    $ AWS_SUBNET_ID=<DEFAULT SUBNET ID>
    $ applicationName=<name of application>
    $ bucketName=<s3 bucket name>
```


## CI/CD

It is implemented using circleci





