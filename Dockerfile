FROM govukpay/openjdk:alpine-3.8.1-jre-base-8.181.13

RUN apk --no-cache upgrade

RUN apk --no-cache add bash gnupg openssl curl

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

ADD target/*.yaml .
ADD target/pay-*-allinone.jar .
ADD docker-startup.sh .
ADD run-with-chamber.sh .
ADD get_bucket_contents.sh . 
ADD fetch-from-pay-secrets.sh . 
ADD run-with-aws-credentials.sh .
CMD bash ./docker-startup.sh
