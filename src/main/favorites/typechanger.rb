#Converts columns of type other than matrix (mostly text, nominal)
#to matrices with a single code containing the same cell content and timestamps
#Note: ordering may change
require 'Datavyu_API.rb'

begin
   count = 0
   allColumns = getColumnList()
   for cName in allColumns
      cur = getColumn(cName)
      if cur.db_var.getRootNode.type != Argument::Type::MATRIX
        puts cur.name + " is of type " + cur.type.to_s
        newCol = createNewColumn(cur.name, "var")
        for cell in cur.cells
            newCell = newCol.make_new_cell()
            newCell.change_arg("onset", cell.onset)
            newCell.change_arg("offset", cell.offset)
            newCell.change_arg("var", cell.argvals[0]) #probably "var", will take first and only  argument whatever it is
            puts "\tCell recreated: " + newCell.onset.to_s + ',' + newCell.offset.to_s+','+newCell.var
        end
        setColumn(newCol)
        puts "\t"+ newCol.name + " recreated as " + getColumn(newCol.name).type.to_s #should say MATRIX
        count += 1
      end
   end
   puts "\number of columns changed: " + count.to_s
   puts "\nFile is NOT saved. It is recommended that you do so as soon as you verify that you are content with the changes."
end
