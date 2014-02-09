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
      for (menu in my.all) {
        h2 {
          img(src:rootURL+menu.iconPath)
          a(href:rootURL+"/thinBackup/"+menu.urlName, menu.displayName)
        }
        div(style:"color:gray; text-decoration:none;") {
          text(menu.description)
        }
      }
    }
  }
}

