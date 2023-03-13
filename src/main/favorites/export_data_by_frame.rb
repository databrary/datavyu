require 'Datavyu_API.rb'

begin
    #$debug = true

    # =================================
    # MODIFY THIS FILE PATH TO CHANGE DUMP LOCATION
    # =================================
    output_filename = "~/Desktop/framebyframe_export.csv"

    column_list = getColumnList()
    columns = Array.new

    # Build header
    puts "Building header..."
    header = Array.new
    for col in column_list
      col = getVariable(col)
      args = col.arglist
      header << col.name + ".ordinal"
      header << col.name + ".onset"
      header << col.name + ".offset"
      header += args.collect { |arg| col.name + "." + arg }
      columns << col
    end

    # Get min and max times
    puts "Getting the minimum and maximium times for the files..."
    min = 99999999999999
    max = 0
    for col in columns
      if col.cells.length > 0
        lmin = col.cells[0].onset
        lmax = col.cells[col.cells.length-1].offset
        if lmin < min
          min = lmin
        end
        if lmax > max
          max = lmax
        end
      end
    end

    # Now go from min to max time in 33ms intervals, getting the cell cellValue at each time
    puts "Getting the data for each time point..."
    time = min
    i = 0
    total_i = (max - min) / 33
    output = "time" + ',' + header.join(',') + "\n"
    while time <= max
      output += time.to_s + ','
      for col in columns
        cell = getCellFromTime(col, time)
        if cell != nil
          data = printCellArgs(cell)
          #puts data
        else
          data = [''] * (col.arglist.length + 3)
        end
        output += data.join(',') + ','
      end
      output += "\n"
      time += 33
      i += 1
      if i % 1000 == 0
        puts "On row " + i.to_s + " out of " + total_i.to_s
      end
    end

    puts "Completed building data.  Writing to file " + output_filename
    output_file = File.new(File.expand_path(output_filename), 'a')
    output_file.write(output)
    output_file.flush()
    output_file.close()
    puts "Finished."


end
