#!/usr/bin/python

from CoursioApi import CoursioApi

api = CoursioApi('YOUR_PUBLIC_KEY', 'YOUR_PRIVATE_KEY', 'YOUR_SALT')

# Read dashboard
print api.call('dashboard', 'read')
