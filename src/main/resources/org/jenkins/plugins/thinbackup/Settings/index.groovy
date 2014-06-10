package org.jvnet.hudson.plugins.thinbackup.Settings

def f=namespace(lib.FormTagLib)
def l=namespace(lib.LayoutTagLib)

l.layout(norefresh:true, permission:app.ADMINISTER, title:my.displayName) {
  l.main_panel {
    h1 {
      img(src:rootURL+my.iconPath)
      text("ThinBackup Configuration")
    }

    f.form { //(method:"POST", action:"saveSettings") {
      f.entry(title:"Configurations") {
        f.repeatableProperty(field:"configurations")
      }
//      
//      f.bottomButtonBar {
//        f.submit(value:_("Save"))
//        f.apply()
//      }
    }
  }
}