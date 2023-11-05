#!/bin/bash

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /$className;format="decap"$/:period                  controllers.$className$Controller.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /$className;format="decap"$/:period                  controllers.$className$Controller.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /change$className$/:period                        controllers.$className$Controller.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /change$className$/:period                        controllers.$className$Controller.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "$className;format="decap"$.title = $className$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $className$" >> ../conf/messages.en
echo "$className;format="decap"$.checkYourAnswersLabel = $className$" >> ../conf/messages.en
echo "$className;format="decap"$.error.nonNumeric = Enter your $className;format="decap"$ using numbers" >> ../conf/messages.en
echo "$className;format="decap"$.error.required = Enter your $className;format="decap"$" >> ../conf/messages.en
echo "$className;format="decap"$.error.wholeNumber = Enter your $className;format="decap"$ using whole numbers" >> ../conf/messages.en
echo "$className;format="decap"$.error.outOfRange = $className$ must be between {0} and {1}" >> ../conf/messages.en
echo "$className;format="decap"$.change.hidden = $className$" >> ../conf/messages.en

echo "Migration $className;format="snake"$ completed"
