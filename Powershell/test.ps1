$StorageAccountName = "uni1tempstorage"
$StorageAccountkey = "PTWPCQVFb3wSxEdax3kPamxlNrTp1tRXtN/jj9QsoTYHu4Yz6wG8gkCYR1MiCqGaCmZGtxOZNq3mR4ZYFoHnRg=="
$Ctx = New-AzureStorageContext -StorageAccountName $StorageAccountName -StorageAccountKey $StorageAccountKey
$localFileDir="C:\temp\azure\upload\"
$localDestDir="C:\temp\azure\download"
$blobName="a.txt"
$localFile= $localFileDir+$blobName
$containerName = "thirdpartytools"

#Get-AzureStorageBlobContent -Blob "t3.ps1" -Container $containerName -context $Ctx -Destination $localDestDir -Force
Set-AzureStorageBlobContent -File $localFile -Container $containerName -context $Ctx -Blob $blobName -Force
#$SourceDir = Get-AzureStorageContainer -Context $Ctx -Container "thirdpartytools"
<#$Files = Get-AzureStorageBlob $SourceDir
$Files
<#
$DestinationDir = "C:\Users\tanmoyd\Desktop\test"
#$ArchiveDir = $spooler_params.get( "archive_dir_path" )

# loop through the files and process them
foreach($f in $Files) {
 Get-AzureStorageFileContent $f -Destination $DestinationDir -Force
 

 
      # the file is incomplete
   #   if($f.Name.EndsWith("~")) {continue}

      # download file to the destination
#    Get-AzureStorageFileContent $f -Destination $DestinationDir -Force
      # move it to an archive folder
 #   Start-AzureStorageFileCopy -AbsoluteUri $f.Uri.AbsoluteUri -DestFilePath $($ArchiveDir + "/" + $f.Name) -DestShareName $FileShare -DestContext $Ctx -Force
      # delete the file in the source folder
#    Remove-AzureStorageFile $f
}

#>