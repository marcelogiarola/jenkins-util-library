# Utility feature library for Jenkins Pipelines

## Repository structure

```shell
├── README.md
├── example-pipeline
│   └── jenkins-pipeline-homologacao.groovy
└── src
    └── com
        └── redhat
            ├── Util.groovy
            └── Version.groovy
```

- **README.md** - This documentation
- **src/om/redhat/*.groovy** - Library`s groovy source code
- **example-pipeline/jenkins-pipeline-homologacao.groovy**  - An example pipeline using the library

## Deployment

1. Clone this repository so you can edit it`s content to meet your needs, **THIS REPOSITORY MAY BE CHANGED TO MEET MY PERSONAL NEEDS WITHOUT NOTICE**
1. Go to (*Jenkins menu structure*) **Jenkins** > **Manage Jenkins** > **System Setup**
    1. Scroll dow to the section **Global Pipeline Libraries**
    1. Click on **Add** button
    1. Fill in the field for the new library as follows
        1. *Name* - **util** (all lowercase letters)
        1. *Default version* - **1.0.0**
        1. *Load implicitly* - **Leave unckecked**
        1. *Allow default version to be overidden** - **Leave checked**
        1. *Include @Library changes in job recent change* - **Leave checked**
        1. Select the **Modern SCM** radio button
        1. Select the appropriated repository option, **GitHub** radio button for this repository
            1. Add credentials if necessary (for this repositoory **- none -**)
            1. Select the **Repository HTTPS URL** radio button
            1. *Repository HTTPS URL* - **Your repository URL** (**https://github.com/marcelogiarola/jenkins-util-library** for this repository)
            1. Click on **Validate** button
            1. Ajust **Behaviours** to your needs, I usually set:
                1. *Discover branches* - **All branches**
                1. *Discover pull requests from origin* - **Merging the pull request with the current target branch revision**
                1. *Discover pull requests from forks*:
                    1. *Strategy* - **The current pull request revision**
                    1. *Trust* - **Nobody**
1. Click **Save**
 
## Use


