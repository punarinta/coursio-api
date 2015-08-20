import time
from hashlib import sha1
import hmac
import binascii
import json
import httplib2 as http

try:
    from urlparse import urlparse
except ImportError:
    from urllib.parse import urlparse

class CoursioApi:

    # initiate the class
    def __init__(self, publicKey, privateKey, salt = None):

        if (salt is None):
            salt = 'coursio_salt'

        self.publicKey = publicKey
        self.privateKey = privateKey
        self.salt = salt
        self.baseUrl = 'https://t-api.s.coursio.com/api/'
        self.sessionId = None

    def auth(self):

        # compute HMAC
        microtime = str(int(round(time.time() * 1000)))
        rawString = self.publicKey + microtime + self.salt
        hashed = hmac.new(self.privateKey, rawString, sha1)
        hmacString = hashed.hexdigest()

        # setup headers
        headers = {
            'X-Coursio-apikey': self.publicKey,
            'X-Coursio-time': microtime,
            'X-Coursio-random': self.salt,
            'X-Coursio-hmac': hmacString
        }

        # concatenate target
        h = http.Http()

        response, content = h.request (
            urlparse(self.baseUrl + 'auth').geturl(),
            'POST',
            '{"method":"loginHmac"}',
            headers
        )

        result = json.loads(content)
        self.sessionId = result['data']['sessionId']


    def call(self, endpoint, method, data = None):
        if (self.sessionId is None):
            self.auth()

        if (data is None):
            data = 'null'

        h = http.Http()

        response, content = h.request (
            urlparse(self.baseUrl + endpoint).geturl(),
            'POST',
            json.dumps({'method': method, "data": data}),
            {'Token': self.sessionId}
        )

        return json.loads(content)
