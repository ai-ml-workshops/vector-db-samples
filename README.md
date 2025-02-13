## This folder contains samples of how to use Vector DB

#### A very basic example: BareboneMovieRecommendation.java

- Uses PgVector (running locally)
  - Ensure it is already running on localhost, and listening on port 5432
  - Username: postgres  (or set it to how you have set up the user as)
  - Password: <please set your password>

- To run the example:
  `mvn compile exec:java -Dexec.mainClass="one.workshop.BareboneMovieRecommendation"`


