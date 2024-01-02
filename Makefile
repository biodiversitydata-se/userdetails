run:
	docker compose up --detach mysqldb
	./gradlew bootRun

# You need to change dataSource.url in userdetails-config.yml
# to use 'mysqldb' instead of '127.0.0.1' for this to work
run-docker:
	./gradlew clean build
	docker compose build --no-cache
	docker compose up --detach

release:
	../sbdi-install/utils/make-release.sh
