#!groovy
import java.text.SimpleDateFormat

node{
    def mavenHome = '/var/lib/jenkins/maven/bin/mvn'
    def userInputList = []
    stage 'Checkout Sources'
      git branch: 'master',
              credentialsId: 'ff0aca4c-5c80-445b-9de1-3d32c962a301',
              url: 'https://github.com/GuidyuMobile/Azzimov-Search.git'

    stage 'User Input'
//        def console = System.console()
//        def userInput = console.readLine("please enter \n > ")
        def userInput  = input message: "Please choose step which you want to skip. " +
                "If multiple steps then separated by comma..." +
                "1) Clean,2) Compile, 3) Package, 4) Artifact", parameters: [string(defaultValue: '0', name: '')]
        userInputList = userInput.split(',')

    if(!userInputList || !userInputList.contains('1')){
        echo('Clean not skip....')
        stage 'Clean'
        sh "${mavenHome} clean"
    }

    if(!userInputList || !userInputList.contains('2')){
        echo('Compile not skip....')
        stage 'Compile'
        sh "${mavenHome} compile"
    }

    stage 'writing'
            def dateFormat = new SimpleDateFormat("yyyyMMddHHmm")
            def date = new Date()
            def pom = readMavenPom file: 'pom.xml'
            writeFile file: "src/main/resources/versionInfo.txt",
            text: "version = ${pom.version} \n" +
            "Build Number = ${env.BUILD_NUMBER} \n" +
            "Date/TIme = " + dateFormat.format(date)

    /*if(!userInputList || !userInputList.contains('3')){
        echo('Test not skip....')
        stage 'Test'
        sh "${mavenHome} test"
    }*/

    if(!userInputList || !userInputList.contains('4')){
        echo('Build not skip....')
        stage 'Build'
        sh "${mavenHome} package -Dmaven.test.skip=true"
    }

    if(!userInputList || !userInputList.contains('5')){
        echo('Archive not skip....')
        stage 'Archive Artifacts'
        archiveArtifacts artifacts: 'target/*.jar'
    }

}