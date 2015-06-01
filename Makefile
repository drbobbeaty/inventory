#
# Makefile for Inventory
#
ifeq ($(shell uname),Linux)
MAKE = make
LEIN = lein
DEPLOY = bin/deploy
endif
ifeq ($(shell uname),Darwin)
MAKE = make
LEIN = lein
DEPLOY = bin/deploy
endif

#
# These are the locations of the directories we'll use
#
SRC_DIR = src
TEST_DIR = test
TARGET_DIR = target
SQL_DIR = deploy/sql
#
# These are a few of the scripts/commands that are WITHIN the project
#
VER = $(shell head -1 project.clj | sed -e 's/^.* \"//' -e 's/\"//')
#
# These are the machines we'll be deploying to
#
PROD_WEB1 = prospero-experiment-api1.snc1
PROD_WEB2 = prospero-experiment-api2.snc1
UAT_WEB1 = caliban-experiment-api1-uat.snc1
UAT_WEB2 = caliban-experiment-api2-uat.snc1

#
# These are the main targets that we'll be making
#
all: jar tests

jar:
	@ echo 'Building uberjar...'
	@ $(LEIN) uberjar &>/dev/null

build/production:
	@ $(DEPLOY) prod just_build

deploy/production:
	@ $(DEPLOY) prod

start/production:
	@ ssh $(PROD_WEB1) "sudo /usr/local/etc/init.d/inventory start"
	@ echo ''
	@ ssh $(PROD_WEB2) "sudo /usr/local/etc/init.d/inventory start"
	@ echo ''

stop/production:
	@ ssh $(PROD_WEB1) "sudo /usr/local/etc/init.d/inventory stop"
	@ echo ''
	@ ssh $(PROD_WEB2) "sudo /usr/local/etc/init.d/inventory stop"
	@ echo ''

restart/production:
	@ $(MAKE) stop/production
	@ $(MAKE) start/production

#
#
#
build/uat:
	@ $(DEPLOY) uat just_build

deploy/uat:
	@ $(DEPLOY) uat

start/uat:
	@ ssh $(UAT_WEB1) "sudo /usr/local/etc/init.d/inventory start"
	@ echo ''
	@ ssh $(UAT_WEB2) "sudo /usr/local/etc/init.d/inventory start"
	@ echo ''

stop/uat:
	@ ssh $(UAT_WEB1) "sudo /usr/local/etc/init.d/inventory stop"
	@ echo ''
	@ ssh $(UAT_WEB2) "sudo /usr/local/etc/init.d/inventory stop"
	@ echo ''

restart/uat:
	@ $(MAKE) stop/uat
	@ $(MAKE) start/uat

clean:
	@ $(LEIN) clean
	@ rm -rf $(TARGET_DIR)

tests:
	@ $(LEIN) test

#
# Create the Schema in the default database
#
schema:
	@ for f in $(SQL_DIR)/create_*.sql ; do \
		echo "running $$f..." ; \
		psql -d inventory -f $$f ; \
	  done
