unit-test:
	sbt test

it-test:
	sbt IntegrationTest/test

all-test: unit-test it-test


qdrant:
	docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant

start:
	sbt "webApp/runMain plagiarism.web.appServer localhost 6334 8080"

# TODO fix paths
populate:
	sbt "webApp/runMain plagiarism.web.populate \
		../lab3-repos/student_submissions \
		../lab3-repos/teacher_repo \
		localhost \
		6334"
