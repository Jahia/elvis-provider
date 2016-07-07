# Elvis External Provider

### INFORMATION
This module allow you to create a mount point in your Digital Factory in order to browse and search your folders/files on your Elvis Server.  
**Note:** Elvis collection are displayed as an empty folder.

### MINIMAL REQUIREMENTS
* Digital Factory 7.1.0.1
* DF Module - External data provider V3.0.1 
* Elvis DAM V5
* **Important:** to use this module you must create yourself a table in your database before deploying it on your instance.  
You will find the script to use [here](https://github.com/Jahia/elvis-provider/tree/master/src/main/resources/META-INF/db) (choose the one related to your DB)

### PROVIDED MAPPING
By default the module as a default mapping for all files and three extended mapping for images, documents and pdf.
It is possible to add properties to those mapping by just updating the `spring.xml`, it is also possible to add new mappings, to do that just follow the pattern of the existing mappings.

*e.g* if you want to add a property to the default mapping, you need to copy this line and modify `p:elvisName` and `p:jcrName` with the name of the property you want to add
```xml
    <bean p:elvisName="mimeType" p:jcrName="jcr:mimeType"
      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
```

The following is the default mapping for file:
```xml
    <bean class="org.jahia.modules.external.elvis.ElvisTypeMapping" id="elvisFile"
          p:jcrName="jnt:file,elvismix:file" p:elvisName="file">
        <property name="properties">
            <list>
                <bean p:elvisName="description" p:jcrName="jcr:description"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>

                <bean p:elvisName="assetCreator" p:jcrName="jcr:createdBy"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="assetCreated" p:jcrName="jcr:created"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>

                <bean p:elvisName="assetModifier" p:jcrName="jcr:lastModifiedBy"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="assetModified" p:jcrName="jcr:lastModified"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>

                <bean p:elvisName="mimeType" p:jcrName="jcr:mimeType"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
            </list>
        </property>
    </bean>
```

**Note:** following mapping is for images and is extending `elvisFile` which is the default mapping, so if you want to add a mapping, you should at least extend `elvisFile`
```xml
    <bean class="org.jahia.modules.external.elvis.ElvisTypeMapping" parent="elvisFile" id="elvisImage"
          p:jcrName="jmix:image,jmix:exif" p:elvisName="image">
        <property name="properties">
            <list merge="true">
                <bean p:elvisName="width" p:jcrName="j:width"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="height" p:jcrName="j:height"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="colorSpace" p:jcrName="j:colorSpace"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="orientation" p:jcrName="j:orientation"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="resolutionUnit" p:jcrName="j:resolutionUnit"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="resolutionX" p:jcrName="j:xresolution"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
                <bean p:elvisName="resolutionY" p:jcrName="j:yresolution"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
            </list>
        </property>
    </bean>
```


**Note:** following mapping is for documents/pdf is extending `elvisFile`
```xml
    <bean class="org.jahia.modules.external.elvis.ElvisTypeMapping" parent="elvisFile" id="elvisDocument"
          p:jcrName="jmix:document" p:elvisName="document,pdf">
        <property name="properties">
            <list merge="true">
                <bean p:elvisName="numberOfPages" p:jcrName="j:pageCount"
                      class="org.jahia.modules.external.elvis.ElvisPropertyMapping"/>
            </list>
        </property>
    </bean>
```