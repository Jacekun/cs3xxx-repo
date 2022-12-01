import os
import re

# Regex to find string
findVersion: str = "(?<=version =)(.*)"

def inc_version(path: str):
    try:
        # Save current contents
        text: str = ""
        version: int = 0
        new_version: int = 0
        # Check file if exists
        #print(f"Checking filepath => {path}")
        if os.path.exists(path):
            # Read contents
            with open(path, "r", encoding='utf-8') as file:
                #print("Read file..")
                text: str = file.read()

                # Iterate over string
                for t in text.split('\n'):
                    if t.startswith("version"):
                        try:
                            version = int(t.split("=", 1)[1].strip())
                            new_version = version + 1
                            print(f"Version: {version} => {new_version}")
                        except Exception as ex:
                            print("Error => {0}: {1}".format(t, ex))
                        break
                
                # Close file
                file.close()
                #print("Reading file closed!")
            
            # Update version on file
            with open(path, "w", encoding='utf-8') as file:
                print("Replacing file contents..")
                newText: str = re.sub(findVersion, f" {str(new_version)}", text)
                #newText: str = text.replace("com.lagradost.cloudstream3", newAppPackage)
                #print("New text => {0}".format(newText))
                file.truncate(0)
                print("File cleared!")
                file.write(newText)
                print("Done writing!")
                file.close()
                print("File closed!")

    except Exception as ex:
        print("Error => {0}: {1}".format(path, ex))
    
if __name__ == '__main__':
    for name in os.listdir("."):
        if os.path.isdir(name):
            #print(f"Folder name: {name}")

            if name == "Example":
                continue

            filepath = os.path.join(name, "build.gradle.kts")

            if os.path.exists(filepath):
                print(f"Gradle exist => {filepath}")

                # Replace language to english
                inc_version(filepath)
