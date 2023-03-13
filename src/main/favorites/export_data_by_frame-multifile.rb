require 'Datavyu_API.rb'

def getCellFromTime(col, time)
  for cell in col.cells
    if cell.onset <= time and cell.offset >= time
      return cell
    end
  end
  return nil
end

def printCellArgs(cell)
  s = Array.new
  s << cell.ordinal.to_s
  s << cell.onset.to_s
  s << cell.offset.to_s
  for arg in cell.arglist
    s << cell.get_arg(arg)
  end
  return s
end

begin
    #$debug = true

    # Ok, we want to loop thru each frame of the video, and at each frame,
    # print out what the cellValues are.
    # ~ Represents your user folder, e.g. /Users/jesse on a Mac or C:\Users\Jesse on Windows.
    # ~/Desktop is your desktop folder no matter what system you're using.
    # =================================
    # FOLDER WITH DATAVYU FILES IN IT
    # =================================
    filedir = "~/Desktop/datavyu_files/"


    # =================================
    # MODIFY THIS FILE PATH TO CHANGE DUMP LOCATION
    # =================================
    output_filename = "~/Desktop/framebyframe_export.csv"

    # =============================================================================
    # END OF USER CHANGEABLE OPTIONS
    # =============================================================================
    filedir = File.expand_path(filedir)
    files = Dir.new(filedir)
    outfile = File.new(File.expand_path(output_filename), 'w')

    header = Array.new
    for file in files
        if file.include?(".opf") and file[0].chr != '.'

            puts "LOADING DATABASE: " + filedir+file
            $dataStore,proj = load_db(filedir + "/" + file)
            puts "SUCCESSFULLY LOADED"


            column_list = getColumnList()
            columns = Array.new

            # Build header
            if header.length() == 0
                puts "Building header..."
                header = Array.new
                for col in column_list.sort()
                  col = getVariable(col)
                  args = col.arglist
                  header << col.name + ".ordinal"
                  header << col.name + ".onset"
                  header << col.name + ".offset"
                  header += args.collect { |arg| col.name + "." + arg }
                  columns << col
                end
                header << "filename"
                output = "time" + ',' + header.join(',') + "\n"
            else
                output = ""
                for col in column_list.sort()
                    col = getVariable(col)
                    columns << col
                end
            end

            # Get min and max times
            puts "Getting the minimum and maximum times for the files..."
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
              output += file + "\n"
              time += 33
              i += 1
              if i % 1000 == 0
                puts "On row " + i.to_s + " out of " + total_i.to_s
              end
            end

            puts "Completed building data.  Writing to file"
            outfile.write(output)
            output = ""

        end
    end
    outfile.flush()
    outfile.close()
    puts "Finished."
end
