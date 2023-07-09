When Maven prompts for the GPG password, try running the following command before running maven:

	$ echo test | gpg --clearsign

The output should contain a "PGP SIGNATURE"; it can be ignored.
