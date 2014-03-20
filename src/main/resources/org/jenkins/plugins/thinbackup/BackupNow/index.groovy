package org.jvnet.hudson.plugins.thinbackup.ThinBackupMgmtLink

def f=namespace(lib.FormTagLib)
def l=namespace(lib.LayoutTagLib)

l.layout(norefresh:true, permission:app.ADMINISTER, title:my.displayName) {
  l.main_panel {
    h1 {
      img(src:rootURL+my.iconPath)
      text("ThinBackup Manual Backup")
    }

    f.form(method:"POST", action:"saveSettings") {
      div(style:"margin:2em") {
        input(type:"button",class:"yui-button", value:_("Backup"))
      }
    }
  }
}