#!/bin/bash

# Full reindex with mapping for both Japanese and English indices
java -jar target/Utility-sau3-3.0.1.jar sau:indexWithMapping -c w206_ja.conf,w206_en.conf -m docusaurus_ja_mapping.json,docusaurus_en_mapping.json

