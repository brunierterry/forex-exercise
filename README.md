[Terry BRUNIER - 2023/04/14]

# Achieved work

## Work description
The assumptions, analysis and the description of the work done for this exercise are detailed in:

[/forex-proxy/README.md](https://github.com/brunierterry/forex-exercise/blob/master/forex-proxy/README.md)

## Deliverable

### Requirements
You will need to have [Docker Desktop ](https://www.docker.com/products/docker-desktop/) installed on the machine you would like to run the Proxy server.

N.B.:
1. You might need administrator privileges to install required softwares.
2. Commends and examples below are based on default configuration. You should adapt them and update [application.conf](https://github.com/brunierterry/forex-exercise/blob/master/forex-proxy/src/main/resources/application.conf) file accordingly with your system, for example if some ports are already in use.
3. Steps described belows gives commends examples for a unix system (macOS 13.x). You would have to adapt examples to your system.
4. Scala `2.13.5` and Java `17.0.4` on your environment.

#### One Frame service
OneFrame service must be running. Please:
1. Ensure to have [OneFrame's docker image](https://hub.docker.com/r/paidyinc/one-frame) locally with the shel command `docker pull paidyinc/one-frame:latest`
2. Run the service with the command `docker run -p 8080:8080 paidyinc/one-frame`

#### Redis service
Redis server service must be running. Please:
docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
1. Ensure to have [Redis docker image](https://hub.docker.com/r/redis/redis-stack) locally with the shel command `docker pull redis/redis-stack-server:latest`
2. Run the service with the command `docker run -d --name redis-stack -p 6379:6379  redis/redis-stack-server`

### How to run - Option 1: IDE
My implementation and all the commits can be viewed on GitHub and downloaded:

https://github.com/brunierterry/forex-exercise

The `Main.scala` class file can be run on a modern IDE (tested with IntelliJ IDEA). It will bind on the address `0.0.0.0:8081'.

### How to run - Option 2: Scala commend
Download [forex-proxy_BrunierTerry_1.1.0.jar](https://drive.google.com/file/d/1eHWH0ENBhCYtkeXhGTtq2ImbhPkBkPvN/view?usp=share_link) (jar file) from google drive and run it locally:

```shell
# in the folder containing the jar:
scala forex-proxy_BrunierTerry_1.1.0.jar
```

### Send query to Proxy service
Send a `GET` request to `0.0.0.0:8081/rates?from=USD&to=JPY` (`USD` and `JPY` can be replaced by other valid currency codes). You can use shell commend tools like httpie, of UI tools such as Postman.



------

<img src="/paidy.png?raw=true" width=300 style="background-color:white;">

# Paidy Take-Home Coding Exercises

## What to expect?
We understand that your time is valuable, and in anyone's busy schedule solving these exercises may constitute a fairly substantial chunk of time, so we really appreciate any effort you put in to helping us build a solid team.

## What we are looking for?
**Keep it simple**. Read the requirements and restrictions carefully and focus on solving the problem.

**Treat it like production code**. That is, develop your software in the same way that you would for any code that is intended to be deployed to production. These may be toy exercises, but we really would like to get an idea of how you build code on a day-to-day basis.

## How to submit?
You can do this however you see fit - you can email us a tarball, a pointer to download your code from somewhere or just a link to a source control repository. Make sure your submission includes a small **README**, documenting any assumptions, simplifications and/or choices you made, as well as a short description of how to run the code and/or tests. Finally, to help us review your code, please split your commit history in sensible chunks (at least separate the initial provided code from your personal additions).

## The Interview:
After you submit your code, we will contact you to discuss and potentially arrange an in-person interview with some of the team.
The interview will cover a wide range of technical and social aspects relevant to working at Paidy, but importantly for this exercise: we will also take the opportunity to step through your submitted code with you.

## The Exercises:
### 1. [Platform] Build an API for managing users
The complete specification for this exercise can be found in the [UsersAPI.md](UsersAPI.md).

### 2. [Frontend] Build a SPA that displays currency exchange rates
The complete specification for this exercise can be found in the [Forex-UI.md](Forex-UI.md).

### 3. [Platform] Build a local proxy for currency exchange rates
The complete specification for this exercise can be found in the [Forex.md](Forex.md).

### 5. [Platform] Build an API for managing a restaurant
The complete specification for this exercise can be found at [SimpleRestaurantApi.md](SimpleRestaurantApi.md)

## F.A.Q.
1) _Is it OK to share your solutions publicly?_
Yes, the questions are not prescriptive, the process and discussion around the code is the valuable part. You do the work, you own the code. Given we are asking you to give up your time, it is entirely reasonable for you to keep and use your solution as you see fit.

2) _Should I do X?_
For any value of X, it is up to you, we intentionally leave the problem a little open-ended and will leave it up to you to provide us with what you see as important. Just remember to keep it simple. If it's a feature that is going to take you a couple of days, it's not essential.

3) _Something is ambiguous, and I don't know what to do?_
The first thing is: don't get stuck. We really don't want to trip you up intentionally, we are just attempting to see how you approach problems. That said, there are intentional ambiguities in the specifications, mainly to see how you fill in those gaps, and how you make design choices.
If you really feel stuck, our first preference is for you to make a decision and document it with your submission - in this case there is really no wrong answer. If you feel it is not possible to do this, just send us an email and we will try to clarify or correct the question for you.

Good luck!
