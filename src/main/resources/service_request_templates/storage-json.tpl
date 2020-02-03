{
    "General": {
        "Researcher": {
            "displayName": "$displayName$",
            "requestorUpi": "$requestorUpi$",
            "mail": "$mail$"
        },
        "requestType": "$requestType$"
    },
    $if(title)$
    "Project": {
        "title": "$title$",
        "abstract": "$abstract$",
        "storageOptions": "$storageOptions$",
        "endDate": "$endDate$",
        "fieldOfResearch": "$fieldOfResearch$"
    },
    "DataInfo": {
        "shortName": "$shortName$",
        "dataRequirements": "$dataRequirements$",
        "projectMembers": "$projectMembers$"
    },
    "DataSize": {
        "sizeThisYear":  "$sizeThisYear$",
        "sizeNextYear": "$sizeNextYear$",
        "comments": "$comments$"
    }
    $else$
    "RequestDetails": {
        "existingFolderName": "$existingFolderName$",
        "updateType": "$updateType$",
        "requestDetails": "$requestDetails$",
        "comments": "$comments$"
    }
    $endif$
}