import csv
import string


with open('vehicles_original.csv', newline='') as csvfile:
    with open("vehicles.csv", "wt") as fout:
        reader = csv.DictReader(csvfile, delimiter=',', quotechar='"')
        
        writer = None
        for row in reader:
            if writer is None:
                list_of_column_names = list(row.keys())
                writer = csv.DictWriter(fout, fieldnames=list_of_column_names, delimiter=',', quotechar='"')
                writer.writeheader()
                
            row['manufacturer plaintext'] = string.capwords(row['manufacturer plaintext'])
            row['commercial name'] = string.capwords(row['commercial name'])
            writer.writerow(row)
