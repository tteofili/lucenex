Docker Compose for Elasticsearch 9.5.0 and Kibana

Run locally for demo/testing:
1) Install Docker and Docker Compose (or Docker with Compose v2)
2) docker compose -f docker-compose.elasticsearch-9.5.0.yml up --detach
3) Wait until Elasticsearch is healthy (check http://localhost:9200)
4) Open Kibana at http://localhost:5601
