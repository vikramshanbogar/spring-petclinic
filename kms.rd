
$ echo -n petclinic | base64
cGV0Y2xpbmlj


$ aws kms encrypt   --key-id 76e14095-03c6-494f-b7f3-8550cd902184   --plaintext "cGV0Y2xpbmlj"   --output text   --query CiphertextBlob
