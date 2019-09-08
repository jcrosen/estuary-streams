FROM clojure:tools-deps-1.10.0.442
WORKDIR /app
COPY deps.edn /app/
RUN clj -Stree
COPY src /app/src
RUN clj -Stree
CMD ["sh", "-c", "sleep 1 && exec clj -m streams.core"]
