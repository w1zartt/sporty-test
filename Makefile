.PHONY: up infra app-local down logs

up:
	docker compose up --build -d

infra:
	docker compose up --build -d kafka schema-registry event-source

app-local:
	cd testapp && ./gradlew bootRun --args='--spring.profiles.active=dev'

down:
	docker compose down

logs:
	docker compose logs -f
