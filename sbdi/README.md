# Userdetails

## Setup

### Config and data directory
Create data directory at `/data/userdetails` and populate as below (it is easiest to symlink the config files to the ones in this repo):
```
mats@xps-13:/data/userdetails$ tree
.
└── config
    └── userdetails-config.yml -> /home/mats/src/biodiversitydata-se/userdetails/sbdi/data/config/userdetails-config.yml
```

### Database
You will need an existing database. It can be imported from production or created by the [ala-cas-5](https://github.com/biodiversitydata-se/ala-cas-5) project.

## Usage
Run locally:
```
make run
```

Build and run in Docker (using Tomcat). This requires a small change in the config file to work. See comment in Makefile.
```
make run-docker
```

Make a release. This will create a new tag and push it. A new Docker container will be built on Github.
```
mats@xps-13:~/src/biodiversitydata-se/userdetails (master *)$ make release

Current version: 1.0.1. Enter the new version (or press Enter for 1.0.2): 
Updating to version 1.0.2
Tag 1.0.2 created and pushed.
```
