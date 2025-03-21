Name "GSEA"

OutFile "GSEA_Win_@VERSION@@WITH_JAVA@-installer.exe"
InstallDir "$PROGRAMFILES64\GSEA_@VERSION@"

ShowInstDetails nevershow
ShowUninstDetails nevershow
SetCompressor /solid lzma
AutoCloseWindow true
Icon "GSEA_@VERSION@\GSEA_Icon.ico"
LicenseData LICENSE_WIN.txt
LicenseForceSelection radiobuttons

Page license
Page directory
Page instfiles
UninstPage instfiles

section
     SetShellVarContext all
     setOutPath "$INSTDIR"
     File /a /r GSEA_@VERSION@\*.*
     createShortCut "$DESKTOP\GSEA_@VERSION@.lnk" "$INSTDIR\gsea.bat" "" "$INSTDIR\GSEA_Icon.ico"
     createDirectory "$SMPROGRAMS\GSEA_@VERSION@"
     createShortCut "$SMPROGRAMS\GSEA_@VERSION@\GSEA.lnk" "$INSTDIR\gsea.bat" "" "$INSTDIR\GSEA_Icon.ico"
     
     WriteUninstaller $INSTDIR\uninstaller.exe
     createShortCut "$SMPROGRAMS\GSEA_@VERSION@\uninstaller.lnk" "$INSTDIR\uninstaller.exe"
sectionEnd

Function un.onInit
    MessageBox MB_YESNO "This will uninstall GSEA_@VERSION@.  Continue?" /SD IDYES IDYES NoAbort
      Abort ; causes uninstaller to quit.
    NoAbort:
FunctionEnd

#RequestExecutionLevel admin

section "Uninstall"
    SetShellVarContext all
	setAutoClose true
	Delete "$SMPROGRAMS\GSEA_@VERSION@\GSEA.lnk"
	Delete "$SMPROGRAMS\GSEA_@VERSION@\uninstaller.lnk"
	RMDir /r "$SMPROGRAMS\GSEA_@VERSION@"
	Delete "$Desktop\GSEA_@VERSION@.lnk"
	
	# NSIS bset-practice recommends not using RMDir /r $INSTDIR... 
	RMDir /r /REBOOTOK $INSTDIR\*.*
	RMDir /REBOOTOK $INSTDIR
sectionEnd