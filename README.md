# ObfuscationAPI

Java API used to apply code obfuscation techniques to APK through an automated pipeline that decompiles, transforms and
recompiles the android application.

This API has been created taking inspiration from the projects of
[Unisannio](https://github.com/faber03/AndroidMalwareEvaluatingTools) and
[Univeristy of Genoa](https://github.com/Mobile-IoT-Security-Lab/Obfuscapk)

## Install

```git clone https://github.com/larchermat/ObfuscationAPI.git```

## Usage

To run the main pipeline, create a main method and then create an instance of the Obfuscation class in the package
src/main/java/it/unibz/obfuscationapi/Obfuscation, then instantiate a Transformation object, and call the
applyTransformation method of the Obfuscation object. Now in the decompiled/{appName}/dist/{obfuscationName} folder
there will be an obfuscated compiled version of the original APK

If one intends to use the extended pipeline for producing the logs of the executions, instead of manually calling the
applyTransformation method, simply call the methods to add the desired transformations and then call startSampling. The
logs will be generated under the logs folder in the root directory; once the logs are created, the LogParser class can
be used to generate trace.xes files parsing the log.txt files

## Repository structure

```
+ apksigner                                     # contains the apksigner jar used to sign the recompiled APK
+ apktool                                       # contains the apktool jar used to decompile and recompile the APK
+ binaries                                      # contains the versions of the dexdump binary for the different OS's
+ scripts                                       # contains the cmd and bash scripts that work on the APK and on the AVD
+ src/main/java/it/unibz/obfuscationapi
|-- + Events                                    # contains all the activity event related classes
|-- + Obfuscation                               # contains the Obfuscation class 
|-- + Transformation                            # contains all the packages of the different obfuscation techniques
|-- + Utility                                   # contains the Utilities and LogParser classes
+ decompiled                                    # directory that contains the decompiled APKs (generated only once the
                                                  pipeline is run
+ errors                                        # directory containing the error logs generated by possible execution
                                                  failures
+ logs                                          # directory containing all logs generated when running the extended
                                                  pipeline
+ traces                                        # directory containing the trace.xes versions of the logs.txt
```