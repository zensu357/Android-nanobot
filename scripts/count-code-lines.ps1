param(
    [string]$Path = "app/src",
    [string[]]$Extensions = @(".kt", ".kts", ".java", ".xml"),
    [switch]$IncludeTests,
    [int]$Top = 10
)

$root = Split-Path -Parent $PSScriptRoot
$targetPath = if ([System.IO.Path]::IsPathRooted($Path)) {
    $Path
} else {
    Join-Path $root $Path
}

if (-not (Test-Path -LiteralPath $targetPath)) {
    Write-Error "Path not found: $targetPath"
    exit 1
}

$normalizedExtensions = $Extensions |
    ForEach-Object {
        if ([string]::IsNullOrWhiteSpace($_)) {
            $null
        } elseif ($_.StartsWith(".")) {
            $_.ToLowerInvariant()
        } else {
            ".{0}" -f $_.ToLowerInvariant()
        }
    } |
    Where-Object { $_ } |
    Select-Object -Unique

if (-not $normalizedExtensions) {
    Write-Error "No valid extensions were provided."
    exit 1
}

$excludedDirs = @("build", ".gradle", ".git")
$testMarkers = @("\src\test\", "\src\androidTest\")

$files = Get-ChildItem -LiteralPath $targetPath -Recurse -File |
    Where-Object {
        $extension = $_.Extension.ToLowerInvariant()
        if ($normalizedExtensions -notcontains $extension) {
            return $false
        }

        foreach ($dir in $excludedDirs) {
            if ($_.FullName -match "[\\/]$([regex]::Escape($dir))([\\/]|$)") {
                return $false
            }
        }

        if (-not $IncludeTests.IsPresent) {
            foreach ($marker in $testMarkers) {
                if ($_.FullName.Contains($marker)) {
                    return $false
                }
            }
        }

        return $true
    }

if (-not $files) {
    Write-Host "No matching files found in $targetPath"
    exit 0
}

$stats = foreach ($file in $files) {
    $lines = (Get-Content -LiteralPath $file.FullName | Measure-Object -Line).Lines
    $relativePath = $file.FullName.Substring($root.Length) -replace '^[\\/]+', ''
    [PSCustomObject]@{
        RelativePath = $relativePath
        Extension = $file.Extension.ToLowerInvariant()
        Lines = $lines
    }
}

$totalLines = ($stats | Measure-Object -Property Lines -Sum).Sum
$fileCount = $stats.Count

Write-Host "Code line statistics"
Write-Host "- Root: $root"
Write-Host "- Scanned path: $targetPath"
Write-Host "- Extensions: $($normalizedExtensions -join ', ')"
Write-Host "- Test files included: $($IncludeTests.IsPresent)"
Write-Host "- Files counted: $fileCount"
Write-Host "- Total lines: $totalLines"
Write-Host ""

Write-Host "Lines by extension"
$stats |
    Group-Object Extension |
    Sort-Object Name |
    ForEach-Object {
        $extensionLines = ($_.Group | Measure-Object -Property Lines -Sum).Sum
        "{0,-8} {1,6} files {2,8} lines" -f $_.Name, $_.Count, $extensionLines
    }

Write-Host ""
Write-Host "Top $Top files"
$stats |
    Sort-Object Lines -Descending |
    Select-Object -First $Top |
    ForEach-Object {
        "{0,8}  {1}" -f $_.Lines, $_.RelativePath
    }
