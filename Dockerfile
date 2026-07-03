FROM eclipse-temurin:21-jre

COPY sync/server/app/build/install/app/ /app/

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/localhost/8080"]

EXPOSE 8080

ENTRYPOINT ["/app/bin/app"]
CMD ["-d", "/data", "-p", "8080"]
