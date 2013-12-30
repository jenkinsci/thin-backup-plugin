package org.jvnet.hudson.plugins.thinbackup.ThinBackupMgmtLink

def f=namespace(lib.FormTagLib)
def l=namespace(lib.LayoutTagLib)
def st=namespace("jelly:stapler")

l.layout(norefresh:true, permission:app.ADMINISTER, title:my.displayName) {
    l.main_panel {
        h1 {
            img(src:"${imagesURL}/48x48/${my.iconFileName}", height:48,width:48)
            text(my.displayName)
        }

        div(style:"margin-left:2em") {
          h2 {
            img(src:rootURL+"/plugin/thinBackup/images/backup.png")
            a(href:rootURL+"/backupNow","Backup Now")
          }
          h2 {
            img(src:rootURL+"/plugin/thinBackup/images/restore.png")
            a(href:rootURL+"/restore","Restore")
          }
          h2 {
            img(src:rootURL+"/plugin/thinBackup/images/settings.png")
            a(href:rootURL+"/settings","Settings")
          }
        }
    }
}

