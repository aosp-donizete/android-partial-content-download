# What is it for?

This is a wrapper example using OkHttp library to download a large file
splitting it and download its multiple parts concurrently.  

It can speed up, theoretically, if a single connection can't establish
a full internet speed connection usage.  

# Hein?
Example:
Your internet speed is 500 units per second (fuck which units are we talking about).  
You have to download a 5000 units file.  
When you establish a connection, only 5 unit is used (because your device or server limitation per connection).  
Meaning you will download it after 1000 seconds.

Now if we establish 10 connections, each one using 5 units per second, we will have 50 units per second  
being downloaded, meaning you will download it after 100 seconds.

But you can handle 500 units per second, right?

So now if you establish 100 connections, each one downloading 5 units per second, you will download it  
after 10 seconds.

Life is great. World is blue and beautiful. We are soon to die. Thanks god!


## HTTP server example
```python
from flask import Flask
from flask import current_app

app = Flask(__name__)

@app.route("/download/<path>")
def download(path):
    return current_app.send_static_file(path)

app.run(debug=True, port=8000, host="0.0.0.0")
```
