#!/bin/bash

# Update both Japanese and English indices with optimized incremental indexing
java -jar target/Utility-sau3-4.0.0.jar sau:indexUpdate -c w206_ja.conf,w206_en.conf --days 3
