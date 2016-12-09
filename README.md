# alexa_sky
Amazon Echo Alexa Skill to control a Sky+ Box (not tested on Sky Q).

It can play recordings from your Planner, and also pause, fast forward, rewind and stop playback. When you 
ask it to play a recording it will pick out the first unwatched episode from the Planner. 

The Sky Box is controlled via UPnP in the same way that the Sky+ app works. In theory the skill
could be extended to do anything the app can do. For example, you could get it to schedule a recording.

# Instructions

## Config
You need to edit the /src/main/resources/config.properties and enter:
1. application_id - this is the application id of your Skill as can be found in the Amazon Developer Console
2. servlet_url - this is the external url of your server along plus the servlet mapping e.g. https://www.joebloggs.com/sky
3. pin_code - this is the Pin Code for your Sky Box, if you have one

## Server side
* _Where?_ This code will run on your local server. It will be called via HTTPS, so needs the SSL port mapped. [Read up on how the web service works](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/developing-an-alexa-skill-as-a-web-service) and what precautions are taken to verify Amazon as the source of requests.
* _Overlay onto the Amazon Alexa Skills Kit SDK & Samples_.  Start by downloading and unpacking the Amazon Alexa Skills Kit SDK & Samples zip file. More information about the Amazon Alexa Skills Kit SDK here: [Amazon Apps and Services Developer Portal](https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/).  Replace files in the SDK with files from this repository. 
* _Rebuild the source._  Build the source with the ./rebuild script.
* _Run the code._ Run with the ./run script.

## Amazon Echo 
* _Create a new Alexa Skill on Amazon's Developer site_.  If you haven't done this before, **stop** and setup at least one of the demos in the Skills Kit SDK and make sure it works with the server configuration you intend to use first.
* Put in the contents of the speechAssets folder into the approriate boxes on the skill's Interaction Model page.
* _Enable the Skill & Test_

# Contents
This includes a modified pom.xml and Launcher.java to support the skills. The pom includes necessary dependencies, e.g. javax.json. 
Launcher.java calls the speechlet.
