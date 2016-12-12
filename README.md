# Alexa Sky
## Overview
An Amazon Echo Alexa Skill to control a Sky+ Box (not tested on Sky Q).

It can play recordings from your Planner, and also pause, fast forward, rewind and stop playback. When you 
ask it to play a recording it will pick out the first unwatched episode from the Planner. 

The Sky Box is controlled via UPnP in the same way that the Sky+ app works. In theory the skill
could be extended to do anything the app can do. For example, you could get it to schedule a recording.

## Requirements
- Raspberry Pi or other home server
- Experience with the RPi Command Line and ability to copy files to the RPi
- External access to RPi (I use no-ip (http://www.noip.com/) as a free way of doing this)
- Sky+ Box (or other device on the local network that you want to control using Alexa)
- Amazon Echo

## Detailed Instructions
Below are detailed steps to set up and run this Custom Skill on your home server and connect it up to the Amazon Developer Portal so that it can be controlled via the Echo.

The advantage of running a skill locally is that it can then be used to control anything on your local network which cannot be easily controlled over the cloud, such as a Sky+ Box or other PVR, or a NAS. I wanted to be able to control my Sky+ Box since this can only be controlled locally using DLNA, not over the internet.

The main components to set up are:

1. Install and run the skill on a Raspberry Pi
2. Set up an AWS Lambda pass-through function to call the skill running on the RPi
3. Configure the skill on the Amazon Developer Console and get it to call the Lambda pass-through

Note that it should also be possible to miss out step (2) and use the Developer Console to point directly to your Raspberry Pi service. However I could not get this to work due to problems with certificates and using the Lambda pass through adds some flexibility, in particular removing the requirement to access the Raspberry Pi on port 443. 

This skill uses port 8889 (this can be changed in ```src/main/java/Launcher.java```) so you need to include this in your ```servlet_url``` in the ```config.properties```. 

Before setting off, I strongly suggest you read through the Alexa Skills Kit [Getting Started Guide](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/getting-started-guide).

### [1. Raspberry Pi](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/developing-an-alexa-skill-as-a-web-service)

- Log in to a Raspberry Pi command prompt
- Install Java 8 ```sudo apt-get install oracle-java8-jdk```
- Make sure correct version is being used ```sudo update-alternatives --config java```
- Download Maven ```wget http://www.mirrorservice.org/sites/ftp.apache.org/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz```
- ```cd /opt```
- Unzip Maven ```sudo tar -xzvf /home/pi/apache-maven-3.3.9-bin.tar.gz```
- Tell the shell where to find maven ```sudoedit /etc/profile.d/maven.sh```
- Enter the following text then save the file
```sh
export M2_HOME=/opt/apache-maven-3.3.9
export PATH=$PATH:$M2_HOME/bin
```
- Log out and back in again so the profile is refreshed
- Test that Maven can be found ```mvn -version```
- You should see something similar to
```sh
Apache Maven 3.3.9 (12a6b3acb947671f09b81f49094c53f426d8cea1; 2016-12-14T17:29:23+00:00)
Maven home: /opt/apache-maven-3.3.9
Java version: 1.8.0, vendor: Oracle Corporation
Java home: /usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt/jre
Default locale: en_GB, platform encoding: UTF-8
OS name: "linux", version: "3.12.26-rt40+", arch: "arm", family: "unix"
```
- Copy the [Alexa Java Samples](https://github.com/amzn/alexa-skills-kit-java) to the ```home/pi/alexa``` folder
- Copy the [Alexa Sky Code](https://github.com/bellissimo/AlexaSky) into the ```home/pi/alexa/samples``` folder. Overwrite files when prompted.
- Follow Amazon's instructions to Create a [Self Signed Certificate](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/testing-an-alexa-skill#h2_sslcert) and [Java Keystore](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/deploying-a-sample-skill-as-a-web-service#h3_keystore)
- ```cd /home/pi/alexa/samples```
- ```nano pom.xml``` and replace the values of ```javax.net.ssl.keyStore``` and ```javax.net.ssl.keyStorePassword``` appropriately
- ```nano /home/pi/alexa/samples/src/main/resources/config.properties```
    - servlet_url - enter the external url of your server plus the servlet mapping (sky) e.g. https://www.joebloggs.com:8889/sky  
    - pin_code - enter the Pin Code for your Sky Box, if you have one
- Build the skill ```mvn assembly:assembly -DdescriptorId=jar-with-dependencies package```

### [2. AWS Lambda Function](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/developing-an-alexa-skill-as-a-lambda-function)

- Go to the Amazon AWS Console and 'Sign In to the Console'
- Switch to 'EU (Ireland)' using the dropdown at the top right
- Navigate to the 'Lambda' Service
- Create a Lamdba function  
    - Upload ```home/pi/alexa/samples/target/alexa-skills-kit-samples-1.0-jar-with-dependencies.jar``` as the Function Package
    - Set ```Java 8``` as the Runtime
    - Set ```skyproxy.SkyProxyRequestStreamHandler``` as the Handler
- Make a note of the generated ARN

### [3. Amazon Developer Console](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/registering-and-managing-alexa-skills-in-the-developer-portal)

- Go to the Amazon Developer Console website and sign in with your Amazon Account
- Convert Amazon Account to a Developer Account if you have not already done so
- Go to the Alexa tab, then Alexa Skills Kit
- Add a New Skill with the following details
    - Skill Type: Custom
    - Language: English (U.K.)
    -  Name: Can be anything
    -  Invocation Name: Whatever you want
    -  Custom Slot Types: 
        - Add new Type called SPEEDS with values of 1, 2, 6, 12, 30
        - Add new Type called PROGRAM_NAMES with values of your favourite program names (e.g. Pointless, The Walking Dead)
    - Intent Schema: Copy this from the Alexa Sky /src/main/java/sky/speechAssets folder
    - Sample Utterances: Copy this from the Alexa Sky /src/main/java/sky/speechAssets folder
    - Service Endpoint Type: The Lambda Function ARN you made a note of earlier
    - Tick Europe and copy the ARN of the Lambda Function into the entry box
    - Account Linking: No
- Save the Skill and make a note of the Application Id
- Go back to the RPi and ```nano /home/pi/alexa/samples/src/main/resources/config.properties```
    - application_id - enter the application id of your Skill that you made a note of just now
- Build the skill ```mvn assembly:assembly -DdescriptorId=jar-with-dependencies package```
- Run the skill ```mvn exec:java -Dexec.executable="java" -DdisableRequestSignatureCheck=true```

All done, you can now test the skill in the Service Simulator or the Echo.