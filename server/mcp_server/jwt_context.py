# Holds the JWT token for the request currently being processed.
# Tool functions can read this value without the token being passed
# through the language model itself.

current_jwt_token = None

def set_current_token(token):
    global current_jwt_token
    current_jwt_token = token

def get_current_token():
    return current_jwt_token