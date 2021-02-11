# Source: https://www.kaggle.com/c/nyc-taxi-trip-duration/data
# train-sample0.05.csv.gz has been generated as a sample of the original train.csv:
awk 'NR==1 || rand()<0.05' train.csv | gzip > train-sample0.1.csv.gz

# NYC neighoubrhoods are from: https://data.beta.nyc/dataset/pediacities-nyc-neighborhoods
