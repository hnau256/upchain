.PHONY: build push

build:
	./gradlew :sync:server:app:installDist --no-daemon -x test --build-cache
	docker build -t hnau256/upchain:latest .

push: build
	docker push hnau256/upchain:latest
