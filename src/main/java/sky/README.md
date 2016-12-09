# Alexa Skill to control a Sky+ Box (not tested with Sky Q)
This skill is built using the ASK and is designed to run on a local server (e.g. Raspberry Pi). You can use the Amazon Developer Console to
either connect to your server directly or you can connect to a Lamda function which acts as a pass-through to pass the JSON to your server.
See skyproxy/SkyProxyRequestStreamHandler for an example of the pass-through code.

It uses UPnP to control the Sky Box in the same way that the Sky+ app works. In theory it can be made to do anything the app can do.

Note that this is designed for a household with a single Sky Box. If you have multiple boxes you would need to amend to include the IP addresses of the boxes.

## Config
Edit the config/config.properties file to set the following:
1. application_id - this is the application id for your skill as found in the Amazon Developer Console
2. servlet_url - this is the url of the servlet endpoint running on your server e.g. https://www.joebloggs.com/sky
3. pin_code - this is the pin code of your Sky+ Box

## Setup
You need to compile and run it on your server, then hook it up to your skill in the Amazon Developer Console. The steps to do this are all documented
here https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/getting-started-guide and the samples are kept here
https://github.com/amzn/alexa-skills-kit-java

## Usage
You can use this skill to play recordings from the Planner. It will pick out the next episode in the series, ignoring
any previously watched or deleted items. It can also pause, stop, fast forward and rewind. It would also be possible to
get it to set up a recording.

See the SampleUtterances for ways to interact with the skill.
