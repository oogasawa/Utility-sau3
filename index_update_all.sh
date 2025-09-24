#!/bin/bash

# Update both Japanese and English indices with optimized incremental indexing
java -jar target/Utility-sau3-3.0.1.jar sau:updateIndex -c w206_ja.conf,w206_en.conf --days 3

